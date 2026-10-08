package chaos.s24.base;

import chaos.s24.base.meta.C24RefMeta;
import com.rosetta.model.lib.RosettaModelObject;
import com.rosetta.model.lib.RosettaModelObjectBuilder;
import com.rosetta.model.lib.annotations.Accessor;
import com.rosetta.model.lib.annotations.AccessorType;
import com.rosetta.model.lib.annotations.Required;
import com.rosetta.model.lib.annotations.RosettaAttribute;
import com.rosetta.model.lib.annotations.RosettaDataType;
import com.rosetta.model.lib.annotations.RuneAttribute;
import com.rosetta.model.lib.annotations.RuneDataType;
import com.rosetta.model.lib.meta.RosettaMetaData;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.process.BuilderMerger;
import com.rosetta.model.lib.process.BuilderProcessor;
import com.rosetta.model.lib.process.Processor;
import java.util.Objects;

import static java.util.Optional.ofNullable;

/**
 * Helper - relocated by the import axis.
 * @version 1.0.0
 */
@RosettaDataType(value="C24Ref", builder=C24Ref.C24RefBuilderImpl.class, version="1.0.0")
@RuneDataType(value="C24Ref", model="chaos", builder=C24Ref.C24RefBuilderImpl.class, version="1.0.0")
public interface C24Ref extends RosettaModelObject {

	C24RefMeta metaData = new C24RefMeta();

	/*********************** Getter Methods  ***********************/
	String getMark();

	/*********************** Build Methods  ***********************/
	C24Ref build();
	
	C24Ref.C24RefBuilder toBuilder();
	
	static C24Ref.C24RefBuilder builder() {
		return new C24Ref.C24RefBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends C24Ref> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends C24Ref> getType() {
		return C24Ref.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processor.processBasic(path.newSubPath("mark"), String.class, getMark(), this);
	}
	

	/*********************** Builder Interface  ***********************/
	interface C24RefBuilder extends C24Ref, RosettaModelObjectBuilder {
		C24Ref.C24RefBuilder setMark(String mark);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processor.processBasic(path.newSubPath("mark"), String.class, getMark(), this);
		}
		

		C24Ref.C24RefBuilder prune();
	}

	/*********************** Immutable Implementation of C24Ref  ***********************/
	class C24RefImpl implements C24Ref {
		private final String mark;
		
		protected C24RefImpl(C24Ref.C24RefBuilder builder) {
			this.mark = builder.getMark();
		}
		
		@Override
		@RosettaAttribute("mark")
		@Accessor(AccessorType.GETTER)
		@Required
		@RuneAttribute("mark")
		public String getMark() {
			return mark;
		}
		
		@Override
		public C24Ref build() {
			return this;
		}
		
		@Override
		public C24Ref.C24RefBuilder toBuilder() {
			C24Ref.C24RefBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(C24Ref.C24RefBuilder builder) {
			ofNullable(getMark()).ifPresent(builder::setMark);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C24Ref _that = getType().cast(o);
		
			if (!Objects.equals(mark, _that.getMark())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (mark != null ? mark.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "C24Ref {" +
				"mark=" + this.mark +
			'}';
		}
	}

	/*********************** Builder Implementation of C24Ref  ***********************/
	class C24RefBuilderImpl implements C24Ref.C24RefBuilder {
	
		protected String mark;
		
		@Override
		@RosettaAttribute("mark")
		@Accessor(AccessorType.GETTER)
		@Required
		@RuneAttribute("mark")
		public String getMark() {
			return mark;
		}
		
		@RosettaAttribute("mark")
		@Accessor(AccessorType.SETTER)
		@Required
		@RuneAttribute("mark")
		@Override
		public C24Ref.C24RefBuilder setMark(String _mark) {
			this.mark = _mark == null ? null : _mark;
			return this;
		}
		
		@Override
		public C24Ref build() {
			return new C24Ref.C24RefImpl(this);
		}
		
		@Override
		public C24Ref.C24RefBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C24Ref.C24RefBuilder prune() {
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getMark()!=null) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C24Ref.C24RefBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			C24Ref.C24RefBuilder o = (C24Ref.C24RefBuilder) other;
			
			
			merger.mergeBasic(getMark(), o.getMark(), this::setMark);
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C24Ref _that = getType().cast(o);
		
			if (!Objects.equals(mark, _that.getMark())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (mark != null ? mark.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "C24RefBuilder {" +
				"mark=" + this.mark +
			'}';
		}
	}
}
