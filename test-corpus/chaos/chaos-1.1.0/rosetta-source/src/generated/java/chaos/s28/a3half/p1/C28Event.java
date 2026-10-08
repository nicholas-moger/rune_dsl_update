package chaos.s28.a3half.p1;

import chaos.s28.a3half.p1.meta.C28EventMeta;
import com.rosetta.model.lib.RosettaModelObject;
import com.rosetta.model.lib.RosettaModelObjectBuilder;
import com.rosetta.model.lib.annotations.Accessor;
import com.rosetta.model.lib.annotations.AccessorType;
import com.rosetta.model.lib.annotations.RosettaAttribute;
import com.rosetta.model.lib.annotations.RosettaDataType;
import com.rosetta.model.lib.annotations.RuneAttribute;
import com.rosetta.model.lib.annotations.RuneDataType;
import com.rosetta.model.lib.meta.RosettaMetaData;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.process.BuilderMerger;
import com.rosetta.model.lib.process.BuilderProcessor;
import com.rosetta.model.lib.process.Processor;
import java.math.BigDecimal;
import java.util.Objects;

import static java.util.Optional.ofNullable;

/**
 * The qualifiable EVENT root - the A7 order-of-load race runs on the Event kind so the s22 Product race stays unmoved.
 * @version 1.0.0
 */
@RosettaDataType(value="C28Event", builder=C28Event.C28EventBuilderImpl.class, version="1.0.0")
@RuneDataType(value="C28Event", model="chaos", builder=C28Event.C28EventBuilderImpl.class, version="1.0.0")
public interface C28Event extends RosettaModelObject {

	C28EventMeta metaData = new C28EventMeta();

	/*********************** Getter Methods  ***********************/
	String getKind();
	BigDecimal getSize();
	C28Extra getExtra();

	/*********************** Build Methods  ***********************/
	C28Event build();
	
	C28Event.C28EventBuilder toBuilder();
	
	static C28Event.C28EventBuilder builder() {
		return new C28Event.C28EventBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends C28Event> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends C28Event> getType() {
		return C28Event.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processor.processBasic(path.newSubPath("kind"), String.class, getKind(), this);
		processor.processBasic(path.newSubPath("size"), BigDecimal.class, getSize(), this);
		processRosetta(path.newSubPath("extra"), processor, C28Extra.class, getExtra());
	}
	

	/*********************** Builder Interface  ***********************/
	interface C28EventBuilder extends C28Event, RosettaModelObjectBuilder {
		C28Extra.C28ExtraBuilder getOrCreateExtra();
		@Override
		C28Extra.C28ExtraBuilder getExtra();
		C28Event.C28EventBuilder setKind(String kind);
		C28Event.C28EventBuilder setSize(BigDecimal size);
		C28Event.C28EventBuilder setExtra(C28Extra extra);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processor.processBasic(path.newSubPath("kind"), String.class, getKind(), this);
			processor.processBasic(path.newSubPath("size"), BigDecimal.class, getSize(), this);
			processRosetta(path.newSubPath("extra"), processor, C28Extra.C28ExtraBuilder.class, getExtra());
		}
		

		C28Event.C28EventBuilder prune();
	}

	/*********************** Immutable Implementation of C28Event  ***********************/
	class C28EventImpl implements C28Event {
		private final String kind;
		private final BigDecimal size;
		private final C28Extra extra;
		
		protected C28EventImpl(C28Event.C28EventBuilder builder) {
			this.kind = builder.getKind();
			this.size = builder.getSize();
			this.extra = ofNullable(builder.getExtra()).map(f->f.build()).orElse(null);
		}
		
		@Override
		@RosettaAttribute("kind")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("kind")
		public String getKind() {
			return kind;
		}
		
		@Override
		@RosettaAttribute("size")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("size")
		public BigDecimal getSize() {
			return size;
		}
		
		@Override
		@RosettaAttribute("extra")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("extra")
		public C28Extra getExtra() {
			return extra;
		}
		
		@Override
		public C28Event build() {
			return this;
		}
		
		@Override
		public C28Event.C28EventBuilder toBuilder() {
			C28Event.C28EventBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(C28Event.C28EventBuilder builder) {
			ofNullable(getKind()).ifPresent(builder::setKind);
			ofNullable(getSize()).ifPresent(builder::setSize);
			ofNullable(getExtra()).ifPresent(builder::setExtra);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C28Event _that = getType().cast(o);
		
			if (!Objects.equals(kind, _that.getKind())) return false;
			if (!Objects.equals(size, _that.getSize())) return false;
			if (!Objects.equals(extra, _that.getExtra())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (kind != null ? kind.hashCode() : 0);
			_result = 31 * _result + (size != null ? size.hashCode() : 0);
			_result = 31 * _result + (extra != null ? extra.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "C28Event {" +
				"kind=" + this.kind + ", " +
				"size=" + this.size + ", " +
				"extra=" + this.extra +
			'}';
		}
	}

	/*********************** Builder Implementation of C28Event  ***********************/
	class C28EventBuilderImpl implements C28Event.C28EventBuilder {
	
		protected String kind;
		protected BigDecimal size;
		protected C28Extra.C28ExtraBuilder extra;
		
		@Override
		@RosettaAttribute("kind")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("kind")
		public String getKind() {
			return kind;
		}
		
		@Override
		@RosettaAttribute("size")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("size")
		public BigDecimal getSize() {
			return size;
		}
		
		@Override
		@RosettaAttribute("extra")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("extra")
		public C28Extra.C28ExtraBuilder getExtra() {
			return extra;
		}
		
		@Override
		public C28Extra.C28ExtraBuilder getOrCreateExtra() {
			C28Extra.C28ExtraBuilder result;
			if (extra!=null) {
				result = extra;
			}
			else {
				result = extra = C28Extra.builder();
			}
			
			return result;
		}
		
		@RosettaAttribute("kind")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("kind")
		@Override
		public C28Event.C28EventBuilder setKind(String _kind) {
			this.kind = _kind == null ? null : _kind;
			return this;
		}
		
		@RosettaAttribute("size")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("size")
		@Override
		public C28Event.C28EventBuilder setSize(BigDecimal _size) {
			this.size = _size == null ? null : _size;
			return this;
		}
		
		@RosettaAttribute("extra")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("extra")
		@Override
		public C28Event.C28EventBuilder setExtra(C28Extra _extra) {
			this.extra = _extra == null ? null : _extra.toBuilder();
			return this;
		}
		
		@Override
		public C28Event build() {
			return new C28Event.C28EventImpl(this);
		}
		
		@Override
		public C28Event.C28EventBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C28Event.C28EventBuilder prune() {
			if (extra!=null && !extra.prune().hasData()) extra = null;
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getKind()!=null) return true;
			if (getSize()!=null) return true;
			if (getExtra()!=null && getExtra().hasData()) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C28Event.C28EventBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			C28Event.C28EventBuilder o = (C28Event.C28EventBuilder) other;
			
			merger.mergeRosetta(getExtra(), o.getExtra(), this::setExtra);
			
			merger.mergeBasic(getKind(), o.getKind(), this::setKind);
			merger.mergeBasic(getSize(), o.getSize(), this::setSize);
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C28Event _that = getType().cast(o);
		
			if (!Objects.equals(kind, _that.getKind())) return false;
			if (!Objects.equals(size, _that.getSize())) return false;
			if (!Objects.equals(extra, _that.getExtra())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (kind != null ? kind.hashCode() : 0);
			_result = 31 * _result + (size != null ? size.hashCode() : 0);
			_result = 31 * _result + (extra != null ? extra.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "C28EventBuilder {" +
				"kind=" + this.kind + ", " +
				"size=" + this.size + ", " +
				"extra=" + this.extra +
			'}';
		}
	}
}
