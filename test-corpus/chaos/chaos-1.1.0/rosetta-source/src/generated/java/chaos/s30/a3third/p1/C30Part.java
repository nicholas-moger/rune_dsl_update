package chaos.s30.a3third.p1;

import chaos.s30.a3third.p1.meta.C30PartMeta;
import com.rosetta.model.lib.GlobalKey;
import com.rosetta.model.lib.RosettaModelObject;
import com.rosetta.model.lib.RosettaModelObjectBuilder;
import com.rosetta.model.lib.annotations.Accessor;
import com.rosetta.model.lib.annotations.AccessorType;
import com.rosetta.model.lib.annotations.Required;
import com.rosetta.model.lib.annotations.RosettaAttribute;
import com.rosetta.model.lib.annotations.RosettaDataType;
import com.rosetta.model.lib.annotations.RuneAttribute;
import com.rosetta.model.lib.annotations.RuneDataType;
import com.rosetta.model.lib.annotations.RuneMetaType;
import com.rosetta.model.lib.meta.RosettaMetaData;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.process.BuilderMerger;
import com.rosetta.model.lib.process.BuilderProcessor;
import com.rosetta.model.lib.process.Processor;
import com.rosetta.model.metafields.MetaFields;
import java.util.Objects;

import static java.util.Optional.ofNullable;

/**
 * Keyed component - relocated by the import axis.
 * @version 1.0.0
 */
@RosettaDataType(value="C30Part", builder=C30Part.C30PartBuilderImpl.class, version="1.0.0")
@RuneDataType(value="C30Part", model="chaos", builder=C30Part.C30PartBuilderImpl.class, version="1.0.0")
public interface C30Part extends RosettaModelObject, GlobalKey {

	C30PartMeta metaData = new C30PartMeta();

	/*********************** Getter Methods  ***********************/
	String getPid();
	MetaFields getMeta();

	/*********************** Build Methods  ***********************/
	C30Part build();
	
	C30Part.C30PartBuilder toBuilder();
	
	static C30Part.C30PartBuilder builder() {
		return new C30Part.C30PartBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends C30Part> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends C30Part> getType() {
		return C30Part.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processor.processBasic(path.newSubPath("pid"), String.class, getPid(), this);
		processRosetta(path.newSubPath("meta"), processor, MetaFields.class, getMeta());
	}
	

	/*********************** Builder Interface  ***********************/
	interface C30PartBuilder extends C30Part, RosettaModelObjectBuilder, GlobalKey.GlobalKeyBuilder {
		MetaFields.MetaFieldsBuilder getOrCreateMeta();
		@Override
		MetaFields.MetaFieldsBuilder getMeta();
		C30Part.C30PartBuilder setPid(String pid);
		C30Part.C30PartBuilder setMeta(MetaFields meta);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processor.processBasic(path.newSubPath("pid"), String.class, getPid(), this);
			processRosetta(path.newSubPath("meta"), processor, MetaFields.MetaFieldsBuilder.class, getMeta());
		}
		

		C30Part.C30PartBuilder prune();
	}

	/*********************** Immutable Implementation of C30Part  ***********************/
	class C30PartImpl implements C30Part {
		private final String pid;
		private final MetaFields meta;
		
		protected C30PartImpl(C30Part.C30PartBuilder builder) {
			this.pid = builder.getPid();
			this.meta = ofNullable(builder.getMeta()).map(f->f.build()).orElse(null);
		}
		
		@Override
		@RosettaAttribute("pid")
		@Accessor(AccessorType.GETTER)
		@Required
		@RuneAttribute("pid")
		public String getPid() {
			return pid;
		}
		
		@Override
		@RosettaAttribute("meta")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("meta")
		@RuneMetaType
		public MetaFields getMeta() {
			return meta;
		}
		
		@Override
		public C30Part build() {
			return this;
		}
		
		@Override
		public C30Part.C30PartBuilder toBuilder() {
			C30Part.C30PartBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(C30Part.C30PartBuilder builder) {
			ofNullable(getPid()).ifPresent(builder::setPid);
			ofNullable(getMeta()).ifPresent(builder::setMeta);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C30Part _that = getType().cast(o);
		
			if (!Objects.equals(pid, _that.getPid())) return false;
			if (!Objects.equals(meta, _that.getMeta())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (pid != null ? pid.hashCode() : 0);
			_result = 31 * _result + (meta != null ? meta.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "C30Part {" +
				"pid=" + this.pid + ", " +
				"meta=" + this.meta +
			'}';
		}
	}

	/*********************** Builder Implementation of C30Part  ***********************/
	class C30PartBuilderImpl implements C30Part.C30PartBuilder {
	
		protected String pid;
		protected MetaFields.MetaFieldsBuilder meta;
		
		@Override
		@RosettaAttribute("pid")
		@Accessor(AccessorType.GETTER)
		@Required
		@RuneAttribute("pid")
		public String getPid() {
			return pid;
		}
		
		@Override
		@RosettaAttribute("meta")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("meta")
		@RuneMetaType
		public MetaFields.MetaFieldsBuilder getMeta() {
			return meta;
		}
		
		@Override
		public MetaFields.MetaFieldsBuilder getOrCreateMeta() {
			MetaFields.MetaFieldsBuilder result;
			if (meta!=null) {
				result = meta;
			}
			else {
				result = meta = MetaFields.builder();
			}
			
			return result;
		}
		
		@RosettaAttribute("pid")
		@Accessor(AccessorType.SETTER)
		@Required
		@RuneAttribute("pid")
		@Override
		public C30Part.C30PartBuilder setPid(String _pid) {
			this.pid = _pid == null ? null : _pid;
			return this;
		}
		
		@RosettaAttribute("meta")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("meta")
		@RuneMetaType
		@Override
		public C30Part.C30PartBuilder setMeta(MetaFields _meta) {
			this.meta = _meta == null ? null : _meta.toBuilder();
			return this;
		}
		
		@Override
		public C30Part build() {
			return new C30Part.C30PartImpl(this);
		}
		
		@Override
		public C30Part.C30PartBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C30Part.C30PartBuilder prune() {
			if (meta!=null && !meta.prune().hasData()) meta = null;
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getPid()!=null) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C30Part.C30PartBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			C30Part.C30PartBuilder o = (C30Part.C30PartBuilder) other;
			
			merger.mergeRosetta(getMeta(), o.getMeta(), this::setMeta);
			
			merger.mergeBasic(getPid(), o.getPid(), this::setPid);
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C30Part _that = getType().cast(o);
		
			if (!Objects.equals(pid, _that.getPid())) return false;
			if (!Objects.equals(meta, _that.getMeta())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (pid != null ? pid.hashCode() : 0);
			_result = 31 * _result + (meta != null ? meta.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "C30PartBuilder {" +
				"pid=" + this.pid + ", " +
				"meta=" + this.meta +
			'}';
		}
	}
}
